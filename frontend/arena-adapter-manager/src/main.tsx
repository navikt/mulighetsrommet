import React from "react";
import ReactDOM from "react-dom/client";
import { Toaster } from "react-hot-toast";
import "@navikt/ds-css";
import { Routes } from "./Routes";

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <Toaster position="top-right" reverseOrder={false} />
    <Routes />
  </React.StrictMode>,
);
