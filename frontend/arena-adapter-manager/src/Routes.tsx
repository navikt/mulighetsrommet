import { BrowserRouter, Navigate, Route, Routes as ReactRouterRoutes } from "react-router";
import { Layout } from "./components/Layout";
import { ArenaAdapter } from "./pages/ArenaAdapter.tsx";
import { MrApi } from "./pages/MrApi.tsx";
import { Tiltakshistorikk } from "./pages/Tiltakshistorikk.tsx";
import { Tiltaksokonomi } from "./pages/Tiltaksokonomi.tsx";

const apps = [
  {
    name: "arena-adapter",
    path: "/arena-adapter",
    page: <ArenaAdapter />,
  },
  {
    name: "mr-api",
    path: "/mr-api",
    page: <MrApi />,
  },
  {
    name: "tiltakshistorikk",
    path: "/tiltakshistorikk",
    page: <Tiltakshistorikk />,
  },
  {
    name: "tiltaksøkonomi",
    path: "/tiltaksokonomi",
    page: <Tiltaksokonomi />,
  },
];

export function Routes() {
  return (
    <BrowserRouter>
      <ReactRouterRoutes>
        <Route path="/" element={<Layout apps={apps} />}>
          {apps.map((app) => (
            <Route key={app.name} path={app.path} element={app.page} />
          ))}
          <Route path="/" element={<Navigate to={apps[0].path} />} />
        </Route>
      </ReactRouterRoutes>
    </BrowserRouter>
  );
}
