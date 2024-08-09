import { ApiError } from "../core/api.tsx";

export function ErrorToast(props: { title: string; error?: ApiError | Error }) {
  const status =
    props.error instanceof ApiError ? `${props.error.status} ${props.error.statusText}` : null;
  return (
    <div>
      <p>{props.title}</p>
      {status && (
        <p>
          <code>Status: {status}</code>
        </p>
      )}
      {props.error?.message && (
        <p>
          <code>Message: {props.error.message}</code>
        </p>
      )}
    </div>
  );
}
