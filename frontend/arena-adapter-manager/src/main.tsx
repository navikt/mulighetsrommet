import { ChakraProvider } from "@chakra-ui/react";
import React from "react";
import ReactDOM from "react-dom/client";
import { extendTheme } from "@chakra-ui/react";
import Routes from "./Routes";
import { Toaster } from "react-hot-toast";

const customeTheme = extendTheme({
  colors: {},
  fonts: {},
  fontSizes: {},
  breakpoints: {
    sm: "320px",
    md: "768px",
    lg: "960px",
    xl: "1200px",
  },
});

const theme = extendTheme({ customeTheme });

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ChakraProvider theme={theme}>
      <Toaster position="top-right" reverseOrder={false} />
      <Routes />
    </ChakraProvider>
  </React.StrictMode>,
);
