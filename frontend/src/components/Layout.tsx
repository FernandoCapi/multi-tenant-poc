import { Link, Outlet } from 'react-router'
import { useAuth } from '../auth/useAuth'

export function Layout() {
  const { me, logout } = useAuth()
  return (
    <>
      <header className="topbar">
        <Link to="/" className="brand">Vesta</Link>
        {me && (
          <div className="topbar-user">
            <span>
              {me.displayName} · {me.family.name}
            </span>
            <button type="button" className="button secondary" onClick={logout}>
              Sair
            </button>
          </div>
        )}
      </header>
      <main className="page">
        <Outlet />
      </main>
    </>
  )
}
