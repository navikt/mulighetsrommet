import { createRoot } from "react-dom/client";
import { APPLICATION_NAME } from "@/constants";
import { LokalApp } from "./LokalApp";
import "../../index.css";

if (import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === "true") {
  import("../../mock/worker")
    .then(({ initializeMockServiceWorker }) => {
      return initializeMockServiceWorker();
    })
    .then(render)
    .catch((error) => {
      // eslint-disable-next-line no-console
      console.error("Error occurred while initializing MSW", error);
    });
} else {
  render();
}

function render() {
  const container = document.getElementById(APPLICATION_NAME);
  if (container) {
    const root = createRoot(container);
    root.render(<LokalApp />);
  }
}
