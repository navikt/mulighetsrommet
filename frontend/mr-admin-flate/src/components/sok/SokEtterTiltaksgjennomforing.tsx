import { Search } from "@navikt/ds-react";
import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { mulighetsrommetClient } from "../../api/clients";
import { z } from "zod";
import { TiltaksgjennomforingMedTiltakstype } from "mulighetsrommet-api-client/build/models/TiltaksgjennomforingMedTiltakstype";

const sokeSchema = z.object({
  tiltaksnummer: z.number().positive(),
});

export function SokEtterTiltaksgjennomforing() {
  const navigate = useNavigate();
  const [error, setError] = useState("");
  const [results, setResults] = useState<TiltaksgjennomforingMedTiltakstype[]>(
    []
  );

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

    try {
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
        setResults(result);
        return;
      }

      if (result.length === 1) {
        const { id } = result[0];
        navigate(`/oversikt/${id}`);
      }
    } catch (error) {
      setError("Kunne ikke slå opp på tiltaksnummer. Prøv igjen senere");
    }
  };

  return (
    <div>
      <form onSubmit={onSubmit} style={{ margin: "1rem 0rem", width: "25rem" }}>
        <label htmlFor="search">Slå opp på tiltaksnummer</label>
        <Search
          label="Slå opp på tiltaksnummer"
          variant="primary"
          name="search"
          error={error}
        />
      </form>
      {results.length > 1 ? (
        <div>
          <p>Fant flere treff:</p>
          <ul>
            {results.map((r) => {
              return (
                <li key={r.id}>
                  <Link to={`/oversikt/${r.id}`}>
                    {r.navn} ({r.tiltaksnummer})
                  </Link>
                </li>
              );
            })}
          </ul>
        </div>
      ) : null}
    </div>
  );
}
