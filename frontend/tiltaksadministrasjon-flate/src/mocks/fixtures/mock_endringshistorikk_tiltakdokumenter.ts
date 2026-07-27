import { EndringshistorikkDto } from "@tiltaksadministrasjon/api-client";
import { mockRedaktor } from "./mock_ansatt";

export const mockEndringshistorikkTiltakDokumenter: EndringshistorikkDto = {
  entries: [
    {
      editedAt: new Date(2024, 3, 10, 10, 0).toISOString(),
      editedBy: {
        navIdent: mockRedaktor.navIdent,
        navn: `${mockRedaktor.fornavn} ${mockRedaktor.etternavn}`,
      },
      id: crypto.randomUUID(),
      operation: "Publiserte tiltaksdokument",
    },
    {
      editedAt: new Date(2024, 3, 10, 9, 30).toISOString(),
      editedBy: {
        navIdent: mockRedaktor.navIdent,
        navn: `${mockRedaktor.fornavn} ${mockRedaktor.etternavn}`,
      },
      id: crypto.randomUUID(),
      operation: "Opprettet tiltaksdokument",
    },
  ],
};
