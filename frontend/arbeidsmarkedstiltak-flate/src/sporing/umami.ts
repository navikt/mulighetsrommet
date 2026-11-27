type UmamiEventData = Record<string, string | number | boolean>;

declare global {
  interface Window {
    umami?: {
      track: (eventName: string, eventData: UmamiEventData) => void | Promise<void>;
    };
  }
}

function reportTrackingFailure() {
  // eslint-disable-next-line no-console
  console.error("Umami tracking failed");
}

export function trackUmamiEvent(eventName: string, eventData: UmamiEventData) {
  const umami = window.umami;
  if (!umami) return;

  try {
    const trackingResult = umami.track(eventName, eventData);
    void Promise.resolve(trackingResult).catch(() => {
      reportTrackingFailure();
    });
  } catch {
    reportTrackingFailure();
  }
}
