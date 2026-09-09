import { useMutation } from "@tanstack/react-query";
import { ArrangorflateService, FieldError, PeriodeType } from "@arrangor-utbetalinger/api-client";
import { queryClient } from "~/api/client";
import { tekster } from "~/tekster";

interface OpprettKravParams {
  orgnr: string;
  gjennomforingId: string;
  belop: number;
  periodeStart: string;
  periodeSlutt: string;
  periodeType: PeriodeType;
  kidNummer: string | null;
  vedlegg: File[];
}

interface OpprettKravResult {
  success: boolean;
  id?: string;
  errors?: FieldError[];
}

export function useOpprettKrav() {
  return useMutation({
    mutationFn: async ({
      orgnr,
      gjennomforingId,
      belop,
      periodeStart,
      periodeSlutt,
      periodeType,
      kidNummer,
      vedlegg,
    }: OpprettKravParams): Promise<OpprettKravResult> => {
      const result = await ArrangorflateService.postOpprettKrav({
        path: { orgnr, gjennomforingId },
        body: {
          belop,
          periodeStart,
          periodeSlutt,
          periodeType,
          kidNummer,
          vedlegg,
        },
        client: queryClient,
      });

      const status = result.response?.status;
      if (status === 413) {
        return {
          success: false,
          errors: [
            {
              pointer: "/vedlegg",
              detail: tekster.bokmal.utbetaling.feilmeldinger.vedleggForStort,
            },
          ],
        };
      }
      if (status === 504) {
        return {
          success: false,
          errors: [
            { pointer: "/vedlegg", detail: tekster.bokmal.utbetaling.feilmeldinger.tidsavbrudd },
          ],
        };
      }

      if (result.error) {
        if ("errors" in result.error) {
          return { success: false, errors: result.error.errors as FieldError[] };
        }
        throw result.error;
      }

      return { success: true, id: result.data.id };
    },
  });
}
