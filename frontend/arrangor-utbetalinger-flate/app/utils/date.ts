/**
 * @deprecated Bruk `subDuration(date, {days: number})`
 */
export function subtractDays(date: Date | string, numDays: number): Date {
  const newDate = new Date(date);
  newDate.setDate(newDate.getDate() - numDays);
  return newDate;
}
