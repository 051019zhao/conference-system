const handlerKey = Symbol('ripple-handler')

export default {
  mounted(element) {
    element.classList.add('ripple-surface')
    const onPointerDown = (event) => {
      if (event.button !== 0 || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
      const bounds = element.getBoundingClientRect()
      const size = Math.max(bounds.width, bounds.height) * 2
      const ink = document.createElement('span')
      ink.className = 'ink-ripple'
      ink.style.width = `${size}px`
      ink.style.height = `${size}px`
      ink.style.left = `${event.clientX - bounds.left - size / 2}px`
      ink.style.top = `${event.clientY - bounds.top - size / 2}px`
      element.appendChild(ink)
      ink.addEventListener('animationend', () => ink.remove(), { once: true })
    }
    element[handlerKey] = onPointerDown
    element.addEventListener('pointerdown', onPointerDown, { passive: true })
  },
  unmounted(element) {
    element.removeEventListener('pointerdown', element[handlerKey])
  }
}
