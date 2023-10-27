import { BrowserRouter, Navigate, Route, Routes as ReactRouterRoutes } from "react-router-dom";
import { Layout } from "./components/Layout";
import { MrArenaAdapterManagement } from "./pages/MrArenaAdapterManagement";
import { MrApiManagement } from "./pages/MrApiManagement";

export function Routes() {
  return (
    <BrowserRouter>
      <ReactRouterRoutes>
        <Route path="/" element={<Layout />}>
          <Route path="/mr-arena-adapter" element={<MrArenaAdapterManagement />} />
          <Route path="/mr-api" element={<MrApiManagement />} />
          <Route path="/" element={<Navigate to="/mr-arena-adapter" />} />
        </Route>
      </ReactRouterRoutes>
    </BrowserRouter>
  );
}
