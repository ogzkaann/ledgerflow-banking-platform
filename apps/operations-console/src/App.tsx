import { lazy, Suspense } from "react";
import { Route, Routes } from "react-router-dom";
import { AppShell } from "@/layout/AppShell";
import { LoadingState } from "@/components/States";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { AdminPermissionGate, WritePermissionGate } from "@/auth/PermissionGate";

const OverviewPage = lazy(() =>
  import("@/pages/OverviewPage").then((module) => ({ default: module.OverviewPage })),
);
const AccountsPage = lazy(() =>
  import("@/pages/AccountsPage").then((module) => ({ default: module.AccountsPage })),
);
const CreateAccountPage = lazy(() =>
  import("@/pages/CreateAccountPage").then((module) => ({ default: module.CreateAccountPage })),
);
const AccountDetailPage = lazy(() =>
  import("@/pages/AccountDetailPage").then((module) => ({ default: module.AccountDetailPage })),
);
const TransfersPage = lazy(() =>
  import("@/pages/TransfersPage").then((module) => ({ default: module.TransfersPage })),
);
const CreateTransferPage = lazy(() =>
  import("@/pages/CreateTransferPage").then((module) => ({ default: module.CreateTransferPage })),
);
const TransferDetailPage = lazy(() =>
  import("@/pages/TransferDetailPage").then((module) => ({ default: module.TransferDetailPage })),
);
const NotificationsPage = lazy(() =>
  import("@/pages/NotificationsPage").then((module) => ({ default: module.NotificationsPage })),
);
const DemoLabPage = lazy(() =>
  import("@/pages/DemoLabPage").then((module) => ({ default: module.DemoLabPage })),
);
const SystemPage = lazy(() =>
  import("@/pages/SystemPage").then((module) => ({ default: module.SystemPage })),
);

export function App() {
  return (
    <Suspense fallback={<LoadingState label="Opening LedgerFlow view" />}>
      <Routes>
        <Route element={<AppShell />}>
          <Route index element={<OverviewPage />} />
          <Route path="accounts" element={<AccountsPage />} />
          <Route path="accounts/new" element={<WritePermissionGate><CreateAccountPage /></WritePermissionGate>} />
          <Route path="accounts/:accountId" element={<AccountDetailPage />} />
          <Route path="transfers" element={<TransfersPage />} />
          <Route path="transfers/new" element={<WritePermissionGate><CreateTransferPage /></WritePermissionGate>} />
          <Route path="transfers/:transferId" element={<TransferDetailPage />} />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="demo" element={<AdminPermissionGate><DemoLabPage /></AdminPermissionGate>} />
          <Route path="system" element={<SystemPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
