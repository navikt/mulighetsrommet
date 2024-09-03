import { Link } from "@remix-run/react";

interface Props {
  title: string;
  tilbakeLenke?: {
    navn: string;
    url: string;
  };
}

export function PageHeader({ title, tilbakeLenke }: Props) {
  return (
    <>
      <h1>{title}</h1>
      {tilbakeLenke ? (
        <Link className="mb-5 inline hover:underline" to={tilbakeLenke.url}>
          {tilbakeLenke.navn}
        </Link>
      ) : null}
    </>
  );
}
