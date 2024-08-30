import { configureMockServer } from "./server";

export const configureMock = () => {
  if (typeof document === "undefined") {
    configureMockServer().listen();
  } else {
    // TODO Hvorfor virker ikke denne?
    // configureMockWorker().start();
  }
};
