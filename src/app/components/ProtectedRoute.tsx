import { Navigate } from 'react-router';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const token = localStorage.getItem('authToken');

  if (!token) {
    // Redirect to login if no token
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}
