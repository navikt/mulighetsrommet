const isProduction = window.location.href.includes(".intern.nav.no");
const isDevelopment = window.location.href.includes(".intern.dev.nav.no");

export const getEnvironment = () => {
  if (isProduction) {
    return "production";
  }

  if (isDevelopment) {
    return "development";
  }

  return "local";
};
