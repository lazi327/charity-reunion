import { createServer, request as upstream } from 'node:http'
import { createReadStream, statSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), 'dist')
const indexFile = path.join(root, 'index.html')
if (!statSync(indexFile, { throwIfNoEntry: false })) {
  console.error('缺少 dist/index.html,请先执行 npm run build')
  process.exit(1)
}
const port = Number(process.env.PORT) || 4173
const apiTarget = process.env.API_TARGET || 'http://localhost:8081'
const proxied = /^\/(api|upload)(\/|$)/

const mime = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.map': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.webp': 'image/webp',
  '.ico': 'image/x-icon',
  '.txt': 'text/plain; charset=utf-8',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.mp4': 'video/mp4'
}

function proxyToApi(req, res) {
  const base = new URL(apiTarget)
  const xff = [req.headers['x-forwarded-for'], req.socket.remoteAddress].filter(Boolean).join(', ')
  const upstreamReq = upstream(
    {
      protocol: base.protocol,
      hostname: base.hostname,
      port: base.port || (base.protocol === 'https:' ? 443 : 80),
      path: req.url,
      method: req.method,
      headers: { ...req.headers, host: base.host, 'x-forwarded-for': xff }
    },
    upstreamRes => {
      res.writeHead(upstreamRes.statusCode, upstreamRes.headers)
      upstreamRes.pipe(res)
    }
  )
  upstreamReq.on('error', err => {
    res.writeHead(502, { 'content-type': 'application/json; charset=utf-8' })
    res.end(JSON.stringify({ code: 1, msg: '后端服务不可用: ' + err.message }))
  })
  req.pipe(upstreamReq)
}

function serveFile(req, res, file) {
  const size = statSync(file).size
  res.writeHead(200, {
    'content-type': mime[path.extname(file).toLowerCase()] || 'application/octet-stream',
    'content-length': size
  })
  if (req.method === 'HEAD') return res.end()
  createReadStream(file).pipe(res)
}

createServer((req, res) => {
  if (proxied.test(req.url)) return proxyToApi(req, res)

  let pathname
  try {
    pathname = decodeURIComponent(new URL(req.url, 'http://x').pathname)
  } catch {
    return res.writeHead(400).end('Bad request')
  }

  const file = path.resolve(root, '.' + pathname)
  if (file !== root && !file.startsWith(root + path.sep)) {
    return res.writeHead(403).end('Forbidden')
  }
  const isFile = (() => {
    try {
      return statSync(file).isFile()
    } catch {
      return false
    }
  })()
  if (isFile) return serveFile(req, res, file)

  if (req.method !== 'GET' && req.method !== 'HEAD') {
    return res.writeHead(405).end('Method not allowed')
  }
  return serveFile(req, res, indexFile)
})
  .listen(port, '0.0.0.0', () => {
    console.log(`web listening on :${port}, proxying ${proxied} -> ${apiTarget}`)
  })
