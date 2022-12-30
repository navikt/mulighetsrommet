import {
  BrowserRouter,
  Route,
  Routes as ReactRouterRoutes,
} from "react-router-dom";
import Management from "./pages/Management";
import { Layout } from "./components/Layout";

function Routes() {
  return (
    <BrowserRouter>
      <ReactRouterRoutes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Management />} />
        </Route>
      </ReactRouterRoutes>
    </BrowserRouter>
  );
}

export default Routes;
