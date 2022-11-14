import { ChakraProvider } from "@chakra-ui/react";
import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import { extendTheme } from "@chakra-ui/react";
import Routes from "./Routes";

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
      <Routes />
    </ChakraProvider>
  </React.StrictMode>
);
