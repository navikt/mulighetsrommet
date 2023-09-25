export function useVisForMiljo(miljoer: string[]): boolean {
  const url = window?.location?.host;

  return miljoer.findIndex((el) => url.toLowerCase().includes(el.toLowerCase())) > -1;
}
