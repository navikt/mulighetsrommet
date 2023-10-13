import { Button, HStack } from "@navikt/ds-react";
import { UseMutationResult } from "@tanstack/react-query";
import { Tiltaksgjennomforing, TiltaksgjennomforingRequest } from "mulighetsrommet-api-client";
import { useFormContext } from "react-hook-form";
import styles from "../skjema/Skjema.module.scss";
import { ExclamationmarkTriangleFillIcon } from "@navikt/aksel-icons";

interface Props {
  redigeringsModus: boolean;
  onClose: () => void;
  mutation: UseMutationResult<Tiltaksgjennomforing, unknown, TiltaksgjennomforingRequest, unknown>;
  size?: "small" | "medium";
}
export function TiltaksgjennomforingSkjemaKnapperad({
  redigeringsModus,
  onClose,
  mutation,
  size = "medium",
}: Props) {
  const {
    formState: { errors },
  } = useFormContext();
  const hasErrors = () => Object.keys(errors).length > 0;
  return (
    <HStack align={"center"}>
      {hasErrors() ? (
        <ExclamationmarkTriangleFillIcon
          height={25}
          width={25}
          color="#C30000
"
        />
      ) : null}
      <Button
        size={size}
        className={styles.button}
        onClick={onClose}
        variant="tertiary"
        type="button"
        data-testid="avbryt-knapp"
        disabled={mutation.isLoading}
      >
        Avbryt
      </Button>
      <Button
        size={size}
        className={styles.button}
        type="submit"
        disabled={mutation.isLoading}
        data-testid="lagre-opprett-knapp"
      >
        {mutation.isLoading ? "Lagrer..." : redigeringsModus ? "Lagre gjennomføring" : "Opprett"}
      </Button>
    </HStack>
  );
}
