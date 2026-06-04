import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider } from "react-router-dom";
import { AuthProvider } from "@/lib/auth/AuthContext";
import { ToastProvider } from "@/components/ui/Toast";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import { router } from "./router";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 30_000,
    },
  },
});

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <AuthProvider>
          <ErrorBoundary>
            {/* v7_startTransition : enveloppe les navigations dans React.startTransition.
                Indispensable avec des pages React.lazy + Suspense, sinon React lève l'erreur
                #426 (suspension sur input synchrone) lors d'une navigation. */}
            <RouterProvider router={router} future={{ v7_startTransition: true }} />
          </ErrorBoundary>
        </AuthProvider>
      </ToastProvider>
    </QueryClientProvider>
  );
}
