// The access token lives in sessionStorage: it survives a page reload but not closing the tab (T-02).
// It is exposed to XSS like any web storage; React escapes rendered text and nginx sends a strict CSP.
const TOKEN_KEY = 'vesta.accessToken'

export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  sessionStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  sessionStorage.removeItem(TOKEN_KEY)
}
