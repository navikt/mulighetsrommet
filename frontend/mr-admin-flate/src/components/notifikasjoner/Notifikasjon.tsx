import { BellIcon } from "@navikt/aksel-icons";
import { LinkPanel } from "@navikt/ds-react";

interface NotifikasjonProps {
  tittel: string;
  melding: string;
  href: string;
}

export function Notifikasjon({ tittel, melding, href }: NotifikasjonProps) {
  return (
    <LinkPanel
      href={href}
      className="flex items-center justify-between bg-[var(--a-orange-100)] p-4"
      border={false}
      data-testid="notifikasjoner"
    >
      <div className="flex items-center gap-4">
        <div>
          <span className="flex h-12 w-12 items-center justify-center rounded-full bg-[var(--a-orange-200)]">
            <BellIcon
              className="inline-block text-2xl text-[#b86b00]"
              aria-label="Notifikasjoner"
            />
          </span>
        </div>
        <div>
          <LinkPanel.Title as="h3">{tittel}</LinkPanel.Title>
          <LinkPanel.Description>{melding}</LinkPanel.Description>
        </div>
      </div>
    </LinkPanel>
  );
}
