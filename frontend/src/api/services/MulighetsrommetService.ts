/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Innsatsgruppe } from '../models/Innsatsgruppe';
import type { Tiltaksgjennomforing } from '../models/Tiltaksgjennomforing';
import type { Tiltakstype } from '../models/Tiltakstype';
import type { CancelablePromise } from '../core/CancelablePromise';
import { request as __request } from '../core/request';

export class MulighetsrommetService {

    /**
     * @returns Innsatsgruppe Array of innsatsgrupper.
     * @throws ApiError
     */
    public static getInnsatsgrupper(): CancelablePromise<Array<Innsatsgruppe>> {
        return __request({
            method: 'GET',
            path: `/api/innsatsgrupper`,
        });
    }

    /**
     * @returns Tiltakstype Array of tiltakstyper.
     * @throws ApiError
     */
    public static getTiltakstyper({
        search,
        innsatsgrupper,
    }: {
        /** Search for tiltakstyper **/
        search?: string,
        /** Innsatsgruppefilter **/
        innsatsgrupper?: Array<number>,
    }): CancelablePromise<Array<Tiltakstype>> {
        return __request({
            method: 'GET',
            path: `/api/tiltakstyper`,
            query: {
                'search': search,
                'innsatsgrupper': innsatsgrupper,
            },
        });
    }

    /**
     * @returns Tiltakstype The specified tiltakstype.
     * @throws ApiError
     */
    public static getTiltakstype({
        id,
    }: {
        /** ID **/
        id: number,
    }): CancelablePromise<Tiltakstype> {
        return __request({
            method: 'GET',
            path: `/api/tiltakstyper/${id}`,
            errors: {
                404: `The specified tiltakstype was not found.`,
            },
        });
    }

    /**
     * @returns Tiltaksgjennomforing Tiltaksgjennomføringer for the specified tiltakstype.
     * @throws ApiError
     */
    public static getTiltaksgjennomforingerByTiltakstype({
        id,
    }: {
        /** ID **/
        id: number,
    }): CancelablePromise<Array<Tiltaksgjennomforing>> {
        return __request({
            method: 'GET',
            path: `/api/tiltakstyper/${id}/tiltaksgjennomforinger`,
        });
    }

    /**
     * @returns Tiltaksgjennomforing Array of tiltaksgjennomføringer.
     * @throws ApiError
     */
    public static getTiltaksgjennomforinger(): CancelablePromise<Array<Tiltaksgjennomforing>> {
        return __request({
            method: 'GET',
            path: `/api/tiltaksgjennomforinger`,
        });
    }

    /**
     * @returns Tiltaksgjennomforing The specified tiltaksgjennomføring.
     * @throws ApiError
     */
    public static getTiltaksgjennomforing({
        id,
    }: {
        /** ID **/
        id: number,
    }): CancelablePromise<Tiltaksgjennomforing> {
        return __request({
            method: 'GET',
            path: `/api/tiltaksgjennomforinger/${id}`,
            errors: {
                404: `The specified tiltaksgjennomføring was not found.`,
            },
        });
    }

}