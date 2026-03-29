import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { BodyShort, Button } from "@navikt/ds-react";
import { useState } from "react";

interface Props {
  heading: string;
  body: string;
  children: React.ReactElement<{ onClick?: (e: React.MouseEvent) => void }>;
}

export function ConfirmModal({ heading, body, children }: Props) {
  const [open, setOpen] = useState(false);
  const childOnClick = children.props.onClick;

  return (
    <>
      <div
        onClickCapture={(e) => {
          e.stopPropagation();
          e.preventDefault();
          setOpen(true);
        }}
      >
        {children}
      </div>
      <VarselModal
        open={open}
        handleClose={() => setOpen(false)}
        headingIconType="info"
        headingText={heading}
        body={<BodyShort>{body}</BodyShort>}
        secondaryButton
        primaryButton={
          <Button
            variant="primary"
            onClick={(e) => {
              setOpen(false);
              childOnClick?.(e);
            }}
          >
            Ja, jeg vil fortsette
          </Button>
        }
      />
    </>
  );
}
