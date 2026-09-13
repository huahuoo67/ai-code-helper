import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

export function getChatStreamUrl(memoryId, message) {
  return apiClient.getUri({
    url: '/ai/chat',
    params: { memoryId, message },
  })
}

function abortError() {
  const error = new Error('请求已停止')
  error.name = 'AbortError'
  return error
}

function waitForRender() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

/**
 * Use the browser's native SSE client so every data event is dispatched
 * independently instead of relying on the browser's ReadableStream flush size.
 */
export function streamChat({ memoryId, message, signal, onChunk }) {
  return new Promise((resolve, reject) => {
    const source = new EventSource(getChatStreamUrl(memoryId, message))
    let settled = false
    let receivedMessage = false
    let eventQueue = Promise.resolve()

    const abort = () => finish(abortError())
    const finish = (error) => {
      if (settled) return
      settled = true
      source.close()
      signal?.removeEventListener('abort', abort)
      eventQueue.then(() => (error ? reject(error) : resolve()))
    }

    source.onmessage = ({ data }) => {
      if (!data || data === '[DONE]') {
        if (data === '[DONE]') finish()
        return
      }

      receivedMessage = true
      // Keep event order while yielding after each event so the message paints progressively.
      eventQueue = eventQueue.then(async () => {
        await onChunk(data)
        await waitForRender()
      })
    }

    source.onerror = () => {
      // Spring closes a completed Flux without a [DONE] marker. Once data arrived,
      // treat that close as normal completion; an early error remains visible.
      finish(receivedMessage ? undefined : new Error('SSE 连接失败'))
    }

    if (signal?.aborted) {
      abort()
    } else {
      signal?.addEventListener('abort', abort, { once: true })
    }
  })
}
