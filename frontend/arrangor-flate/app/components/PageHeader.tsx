import { RefusjonskravIkon } from "./icons/RefusjonskravIkon";
import { LinkWithTabState } from "./LinkWithTabState";

interface Props {
  title: string;
  tilbakeLenke?: {
    navn: string;
    url: string;
  };
}

export function PageHeader({ title, tilbakeLenke }: Props) {
  return (
    <div className="flex flex-col relative">
      <span className="hidden lg:inline-block absolute -left-[85px] top-[20px]">
        <RefusjonskravIkon />
      </span>
      <h1 className="mb-2">{title}</h1>
      {tilbakeLenke ? (
        <LinkWithTabState className="mb-5 inline hover:underline" to={tilbakeLenke.url}>
          {tilbakeLenke.navn}
        </LinkWithTabState>
      ) : null}
    </div>
  );
}
