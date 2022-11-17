import { useRouteError } from "react-router-dom";

export function ErrorPage() {
  const error = useRouteError() as
    | { statusText?: string; message?: string }
    | undefined;

  return (
    <div id="error-page">
      <h1>Oops!</h1>
      <p>Her har det skjedd en feil 🥺</p>
      <p>
        <i>{error?.statusText || error?.message}</i>
      </p>
    </div>
  );
}
