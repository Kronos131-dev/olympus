import { createBrowserRouter, Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/lib/auth/AuthContext";
import { AppLayout } from "@/components/AppLayout";
import { FullPageSpinner } from "@/components/ui/misc";
import { lazyWithReload } from "@/lib/lazyWithReload";

const LoginPage = lazyWithReload(() => import("@/pages/LoginPage"));
const RegisterPage = lazyWithReload(() => import("@/pages/RegisterPage"));
const DashboardPage = lazyWithReload(() => import("@/pages/DashboardPage"));
const OraclePage = lazyWithReload(() => import("@/pages/OraclePage"));
const MealsPage = lazyWithReload(() => import("@/pages/MealsPage"));
const MealEditorPage = lazyWithReload(() => import("@/pages/MealEditorPage"));
const AddFoodPage = lazyWithReload(() => import("@/pages/AddFoodPage"));
const PlanPage = lazyWithReload(() => import("@/pages/PlanPage"));
const ProfilePage = lazyWithReload(() => import("@/pages/ProfilePage"));

// N'autorise l'accès qu'aux utilisateurs authentifiés.
function RequireAuth() {
  const { status } = useAuth();
  if (status === "loading") return <FullPageSpinner />;
  if (status === "anonymous") return <Navigate to="/login" replace />;
  return <Outlet />;
}

// Redirige les utilisateurs déjà connectés hors des pages d'auth.
function RedirectIfAuth() {
  const { status } = useAuth();
  if (status === "loading") return <FullPageSpinner />;
  if (status === "authenticated") return <Navigate to="/" replace />;
  return <Outlet />;
}

export const router = createBrowserRouter(
  [
    {
      element: <RedirectIfAuth />,
      children: [
        { path: "/login", element: <LoginPage /> },
        { path: "/register", element: <RegisterPage /> },
      ],
    },
    {
      element: <RequireAuth />,
      children: [
        // Pages plein écran (sans nav) : éditeurs/modaux.
        { path: "/meals/new", element: <MealEditorPage /> },
        { path: "/meals/:id/edit", element: <MealEditorPage /> },
        { path: "/food/add", element: <AddFoodPage /> },
        {
          element: <AppLayout />,
          children: [
            { path: "/", element: <DashboardPage /> },
            { path: "/oracle", element: <OraclePage /> },
            { path: "/meals", element: <MealsPage /> },
            { path: "/plan", element: <PlanPage /> },
            { path: "/profile", element: <ProfilePage /> },
          ],
        },
      ],
    },
    { path: "*", element: <Navigate to="/" replace /> },
  ],
  { basename: "/" },
);
