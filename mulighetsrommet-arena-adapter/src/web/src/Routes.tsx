import {
  BrowserRouter,
  Route,
  Routes as ReactRouterRoutes,
} from "react-router-dom";
import App from "./App";
import Management from "./pages/Management";
import Statistics from "./pages/Statistics";

function Routes() {
  return (
    <BrowserRouter>
      <ReactRouterRoutes>
        <Route path="/" element={<App />}>
          <Route index element={<Statistics />} />
          <Route path="management" element={<Management />} />
        </Route>
      </ReactRouterRoutes>
    </BrowserRouter>
  );
}

export default Routes;
