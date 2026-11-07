// Added to the generated service worker: a tap on a reminder opens the app, or brings it forward if it's open.
self.addEventListener('notificationclick', event => {
  event.notification.close()
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(windows => {
      const open = windows.find(client => new URL(client.url).pathname.startsWith(self.registration.scope.replace(self.location.origin, '')))
      return open ? open.focus() : self.clients.openWindow(self.registration.scope)
    }),
  )
})
