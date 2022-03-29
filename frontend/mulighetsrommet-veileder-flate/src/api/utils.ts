import { hasData as _hasData, WithData, WithError, WithoutData } from '@nutgaard/use-fetch';

type AsyncData<TYPE> = WithoutData | WithData<TYPE> | WithError;

// Midlertidig fix for at hasData fra useFetch sjekker status og ikke at data er lastet inn
export function hasData<TYPE>(result: AsyncData<TYPE>): result is WithData<TYPE> {
  return (result as WithData<TYPE>).data != null && _hasData(result);
}
