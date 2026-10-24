import type { ReactNode } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthProvider'
import { useAuth } from './auth/useAuth'
import { Layout } from './components/Layout'
import { GoalDetailPage } from './pages/GoalDetailPage'
import { GoalsPage } from './pages/GoalsPage'
import { LoginPage } from './pages/LoginPage'
import { NewGoalPage } from './pages/NewGoalPage'

function RequireAuth({ children }: { children: ReactNode }) {
  const { me, loading } = useAuth()
  if (loading) {
    return <p>Carregando…</p>
  }
  return me ? children : <Navigate to="/login" replace />
}

function PublicOnly({ children }: { children: ReactNode }) {
  const { me, loading } = useAuth()
  if (loading) {
    return <p>Carregando…</p>
  }
  return me ? <Navigate to="/" replace /> : children
}

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/login" element={<PublicOnly><LoginPage /></PublicOnly>} />
        <Route path="/" element={<RequireAuth><GoalsPage /></RequireAuth>} />
        <Route path="/goals/new" element={<RequireAuth><NewGoalPage /></RequireAuth>} />
        <Route path="/goals/:goalId" element={<RequireAuth><GoalDetailPage /></RequireAuth>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  )
}
