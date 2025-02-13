import "@testing-library/jest-dom";
import "jest-environment-jsdom";

// Mock ResizeObserver which might be needed for some UI components
global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
};
