import { createBrowserRouter, Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/lib/auth/AuthContext";
import { AppLayout } from "@/components/AppLayout";
import { FullPageSpinner } from "@/components/ui/misc";
import { RouteError } from "@/components/RouteError";
import { lazyWithReload } from "@/lib/lazyWithReload";

const LoginPage = lazyWithReload(() => import("@/pages/LoginPage"));
const RegisterPage = lazyWithReload(() => import("@/pages/RegisterPage"));
const ForgotPasswordPage = lazyWithReload(
  () => import("@/pages/ForgotPasswordPage"),
);
const ResetPasswordPage = lazyWithReload(
  () => import("@/pages/ResetPasswordPage"),
);
const DashboardPage = lazyWithReload(() => import("@/pages/DashboardPage"));
const OraclePage = lazyWithReload(() => import("@/pages/OraclePage"));
const MealsPage = lazyWithReload(() => import("@/pages/MealsPage"));
const MealEditorPage = lazyWithReload(() => import("@/pages/MealEditorPage"));
const AddFoodPage = lazyWithReload(() => import("@/pages/AddFoodPage"));
const MealAnalysisPage = lazyWithReload(
  () => import("@/pages/MealAnalysisPage"),
);
const MicronutrientsPage = lazyWithReload(
  () => import("@/pages/MicronutrientsPage"),
);
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
    // Route racine sans chemin : errorElement global → écran de secours + reload auto sur
    // chunk périmé, au lieu du « Unexpected Application Error! » brut de React Router.
    {
      errorElement: <RouteError />,
      children: [
        {
          element: <RedirectIfAuth />,
          children: [
            { path: "/login", element: <LoginPage /> },
            { path: "/register", element: <RegisterPage /> },
            { path: "/forgot-password", element: <ForgotPasswordPage /> },
            { path: "/reset-password", element: <ResetPasswordPage /> },
          ],
        },
        {
          element: <RequireAuth />,
          children: [
            // Pages plein écran (sans nav) : éditeurs/modaux.
            { path: "/meals/new", element: <MealEditorPage /> },
            { path: "/meals/:id/edit", element: <MealEditorPage /> },
            { path: "/food/add", element: <AddFoodPage /> },
            { path: "/food/analyze", element: <MealAnalysisPage /> },
            {
              element: <AppLayout />,
              children: [
                { path: "/", element: <DashboardPage /> },
                { path: "/oracle", element: <OraclePage /> },
                { path: "/meals", element: <MealsPage /> },
                { path: "/plan", element: <PlanPage /> },
                { path: "/micros", element: <MicronutrientsPage /> },
                { path: "/profile", element: <ProfilePage /> },
              ],
            },
          ],
        },
        { path: "*", element: <Navigate to="/" replace /> },
      ],
    },
  ],
  { basename: "/" },
);
