import { useEndringshistorikk } from "@/api/endringshistorikk/useEndringshistorikk";
import { EndringshistorikkType } from "@tiltaksadministrasjon/api-client";
import React, { ReactElement, useRef, useState } from "react";
import { ClockDashedIcon } from "@navikt/aksel-icons";
import { Button, Loader, Popover } from "@navikt/ds-react";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { ViewEndringshistorikk } from "./ViewEndringshistorikk";

interface EndringshistorikkProps {
  id: string;
  type: EndringshistorikkType;
}

export function Endringshistorikk({ id, type }: EndringshistorikkProps) {
  return (
    <EndringshistorikkPopover>
      <EndringshistorikkView id={id} type={type} />
    </EndringshistorikkPopover>
  );
}

interface EndringshistorikkPopoverProps {
  children: ReactElement;
}

function EndringshistorikkPopover({ children }: EndringshistorikkPopoverProps) {
  const [open, setOpen] = useState(false);
  const buttonRef = useRef<HTMLButtonElement>(null);

  return (
    <>
      <Button
        data-color="neutral"
        ref={buttonRef}
        onClick={() => setOpen(!open)}
        aria-expanded={open}
        variant="tertiary"
        type="button"
        size="small"
        title="Trykk for å se endringshistorikk"
      >
        <ClockDashedIcon height={25} width={25} title="Endringshistorikk" />
      </Button>
      <Popover
        open={open}
        onClose={() => setOpen(false)}
        anchorEl={buttonRef.current}
        placement="bottom-end"
      >
        <Popover.Content style={{ maxHeight: "350px", overflowY: "auto" }}>
          <React.Suspense fallback={<Loader title="Laster endringshistorikk" />}>
            <InlineErrorBoundary>{open ? children : null}</InlineErrorBoundary>
          </React.Suspense>
        </Popover.Content>
      </Popover>
    </>
  );
}

function EndringshistorikkView({ id, type }: EndringshistorikkProps) {
  const { data: historikk } = useEndringshistorikk(id, type);
  return <ViewEndringshistorikk historikk={historikk} />;
}
