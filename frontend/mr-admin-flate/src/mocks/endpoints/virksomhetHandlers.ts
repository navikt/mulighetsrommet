import { rest } from "msw";
import {
  Virksomhet,
  VirksomhetKontaktperson,
} from "mulighetsrommet-api-client";
import { mockVirksomheter } from "../fixtures/mock_virksomheter";
import { mockVirksomhetKontaktperson } from "../fixtures/mock_virksomhet_kontaktperson";

export const virksomhetHandlers = [
  rest.get<any, any, Virksomhet[]>(
    "*/api/v1/internal/virksomhet/sok/:sok",
    (req, res, ctx) => {
      return res(
        ctx.status(200),
        ctx.json(
          mockVirksomheter.filter((enhet) =>
            enhet.navn?.toLowerCase().includes(req.params.sok.toLowerCase())
          )
        )
      );
    }
  ),
  rest.get<any, any, Virksomhet | undefined>(
    "*/api/v1/internal/virksomhet/:orgnr",
    (req, res, ctx) => {
      return res(
        ctx.status(200),
        ctx.json(
          mockVirksomheter.find(
            (enhet) => enhet.organisasjonsnummer === req.params.orgnr
          )
        )
      );
    }
  ),
  rest.get<any, any, Virksomhet[] | undefined>(
    "*/api/v1/internal/virksomhet",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockVirksomheter));
    }
  ),

  rest.get<any, any, VirksomhetKontaktperson[]>(
    "*/api/v1/internal/*/kontaktperson",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockVirksomhetKontaktperson));
    }
  ),
];
