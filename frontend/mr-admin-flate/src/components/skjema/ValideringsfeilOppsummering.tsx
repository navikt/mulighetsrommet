import { ExclamationmarkTriangleFillIcon } from "@navikt/aksel-icons";
import { Button, ErrorSummary, Popover } from "@navikt/ds-react";
import { useRef, useState } from "react";
import { useFormContext } from "react-hook-form";
import styles from "./ValideringsfeilOppsummering.module.scss";

export function ValideringsfeilOppsummering() {
  const visValideringsFeilTrekantRef = useRef(null);
  const [visValideringsfeil, setVisValideringsfeil] = useState(false);
  const {
    formState: { errors },
  } = useFormContext();

  const hentUtValideringsfeil = (obj: any): { message: string; ref: string }[] => {
    let messages: { message: string; ref: string }[] = [];
    for (const key in obj) {
      if (obj[key] !== null && typeof obj[key] === "object") {
        if (obj[key].message && obj[key].ref) {
          messages.push({ message: obj[key].message, ref: obj[key].ref });
        } else {
          messages = messages.concat(hentUtValideringsfeil(obj[key]));
        }
      }
    }

    return messages;
  };

  const messages = hentUtValideringsfeil(errors);

  if (messages.length === 0 && Object.keys(errors).length === 0) return null;

  return (
    <>
      <Button
        variant="tertiary-neutral"
        type="button"
        size="small"
        aria-live="assertive"
        tabIndex={0}
        className={styles.varseltrekant}
        onClick={() => setVisValideringsfeil(true)}
        ref={visValideringsFeilTrekantRef}
        title="Det er valideringsfeil i skjema. Trykk for å få oversikt over valideringsfeilene."
      >
        <ExclamationmarkTriangleFillIcon height={25} width={25} color="#C30000" />
      </Button>
      <Popover
        open={visValideringsfeil}
        onClose={() => setVisValideringsfeil(false)}
        anchorEl={visValideringsFeilTrekantRef.current}
      >
        <Popover.Content>
          <ErrorSummary
            className={styles.valideringsfeil}
            heading="Det er valideringsfeil i skjema"
          >
            {messages.map((value, key) => {
              return (
                <ErrorSummary.Item className={styles.valideringsfeil_item} key={key}>
                  {value.message}
                </ErrorSummary.Item>
              );
            })}
            <pre>{JSON.stringify(errors, null, 2)}</pre>
          </ErrorSummary>
        </Popover.Content>
      </Popover>
    </>
  );
}
