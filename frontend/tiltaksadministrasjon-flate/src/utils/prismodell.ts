import { PrismodellType } from "@tiltaksadministrasjon/api-client";

export function stotterBeskrivelseTilArrangor(prismodell: PrismodellType) {
  switch (prismodell) {
    case PrismodellType.ANNEN_AVTALT_PRIS:
    case PrismodellType.FAST_SATS_PER_BENYTTET_PLASS_PER_MANED:
    case PrismodellType.FAST_SATS_PER_AVTALT_PLASS_PER_MANED:
    case PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_MANED:
    case PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_UKE:
    case PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_HELE_UKE:
    case PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER:
      return true;
    case PrismodellType.ANSKAFFET_ENKELTPLASS:
    case PrismodellType.TILSKUDD_TIL_OPPLAERING:
    case PrismodellType.INGEN_KOSTNADER:
      return false;
  }
}
