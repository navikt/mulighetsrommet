import { Search } from "@navikt/ds-react";
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { mulighetsrommetClient } from "../../api/clients";
import { z } from "zod";

const sokeSchema = z.object({
  tiltaksnummer: z.number().positive(),
});

export function SokEtterTiltaksgjennomforing() {
  const navigate = useNavigate();
  const [error, setError] = useState("");

  const onSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const tiltaksnummer = formData.get("search") as string;

    const parsed = sokeSchema.safeParse({
      tiltaksnummer: parseInt(tiltaksnummer),
    });

    if (!parsed.success) {
      setError("Tiltaksnummer består bare av tall");
      return;
    }

    const result =
      await mulighetsrommetClient.tiltaksgjennomforinger.sokEtterTiltaksgjennomforinger(
        { tiltaksnummer }
      );

    if (!result) {
      setError(
        `Fant ingen tiltaksgjennomføring for tiltaksnummer: ${tiltaksnummer}`
      );
      return;
    }

    if (result.length > 1) {
      setError(
        `Fikk treff på mer enn en tiltaksgjennomføring for tiltaksnummer: ${tiltaksnummer}. Begrens søket mer`
      );
      return;
    }

    if (result.length === 1) {
      const { id } = result[0];
      navigate(`/oversikt/${id}`);
    }
  };

  return (
    <form onSubmit={onSubmit} style={{ margin: "1rem 0rem", width: "25rem" }}>
      <label htmlFor="search">Slå opp på tiltaksnummer</label>
      <Search
        label="Slå opp på tiltaksnummer"
        variant="primary"
        name="search"
        error={error}
      />
    </form>
  );
}
