import { TiltaksgjennomforingerService } from "@mr/api-client";

export async function tiltaksgjennomforingerLoader() {
  const data = await TiltaksgjennomforingerService.getTiltaksgjennomforinger();
  return data;
}
export async function tiltaksgjennomforingerForAvtaleLoader() {
  const data = await TiltaksgjennomforingerService.getTiltaksgjennomforinger();
  return data;
}
