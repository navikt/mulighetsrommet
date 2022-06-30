
export interface Datapunkt {
    tiltakstype: string;
    antallManeder: string;
    'Arbeidstaker m. ytelse/oppf': number;
    'Kun arbeidstaker': number;
    'Registrert hos Nav': number;
    Ukjent: number;
}
declare const datapunkt: Datapunkt[];
export default datapunkt;
