import React from "react";
import {
  BrowserRouter as Router,
  Navigate,
  Route,
  Routes,
  useLocation,
} from "react-router-dom";
import { AuthProvider } from "./context/AuthProvider";
import { useAuth } from "./context/auth-context";
import { LoginPage } from "./pages/LoginPage";
import { Dashboard } from "./components/Dashboard/Dashboard";
import { ClientDetail } from "./components/ClientDetail/ClientDetail";

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
};

const HomeRedirect: React.FC = () => {
  const { isAuthenticated } = useAuth();
  return <Navigate to={isAuthenticated ? "/dashboard" : "/login"} replace />;
};

const AppRoutes: React.FC = () => (
  <Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/" element={<HomeRedirect />} />
    <Route
      path="/dashboard"
      element={
        <ProtectedRoute>
          <Dashboard />
        </ProtectedRoute>
      }
    />
    <Route
      path="/clients/:clientId"
      element={
        <ProtectedRoute>
          <ClientDetail />
        </ProtectedRoute>
      }
    />
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes>
);

export default function App() {
  return (
    <AuthProvider>
      <Router>
        <AppRoutes />
      </Router>
    </AuthProvider>
  );
}
