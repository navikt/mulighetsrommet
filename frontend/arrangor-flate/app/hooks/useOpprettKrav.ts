import { useMutation } from "@tanstack/react-query";
import { ArrangorflateService, FieldError, PeriodeType } from "api-client";
import { queryClient } from "~/api/client";

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
