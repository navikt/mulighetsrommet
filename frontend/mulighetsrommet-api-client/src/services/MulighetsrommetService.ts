/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Innsatsgruppe } from '../models/Innsatsgruppe';
import type { Tiltaksgjennomforing } from '../models/Tiltaksgjennomforing';
import type { Tiltakskode } from '../models/Tiltakskode';
import type { Tiltakstype } from '../models/Tiltakstype';

import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';

export class MulighetsrommetService {

    /**
     * @returns Innsatsgruppe Array of innsatsgrupper.
     * @throws ApiError
     */
    public static getInnsatsgrupper(): CancelablePromise<Array<Innsatsgruppe>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/innsatsgrupper',
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
        innsatsgrupper?: number,
    }): CancelablePromise<Array<Tiltakstype>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/tiltakstyper',
            query: {
                'search': search,
                'innsatsgrupper': innsatsgrupper,
            },
        });
    }

    /**
     * @returns Tiltakstype Created tiltakstype
     * @throws ApiError
     */
    public static createTiltakstype({
        requestBody,
    }: {
        requestBody?: Tiltakstype,
    }): CancelablePromise<Tiltakstype> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/tiltakstyper',
            body: requestBody,
            mediaType: 'application/json',
        });
    }

    /**
     * @returns Tiltakstype the specified tiltakstype.
     * @throws ApiError
     */
    public static getTiltakstype({
        tiltakskode,
    }: {
        /** Tiltakskode **/
        tiltakskode: Tiltakskode,
    }): CancelablePromise<Tiltakstype> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/tiltakstyper/{tiltakskode}',
            path: {
                'tiltakskode': tiltakskode,
            },
            errors: {
                404: `The specified tiltakstype was not found.`,
            },
        });
    }

    /**
     * @returns Tiltakstype Updated tiltakstype
     * @throws ApiError
     */
    public static updateTiltakstype({
        tiltakskode,
        requestBody,
    }: {
        /** Tiltakskode **/
        tiltakskode: Tiltakskode,
        requestBody?: Tiltakstype,
    }): CancelablePromise<Tiltakstype> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/api/tiltakstyper/{tiltakskode}',
            path: {
                'tiltakskode': tiltakskode,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }

    /**
     * @returns Tiltaksgjennomforing An array of tiltaksgjennomføringer for specified tiltakskode.
     * @throws ApiError
     */
    public static getTiltaksgjennomforingerByTiltakskode({
        tiltakskode,
    }: {
        /** Tiltakskode **/
        tiltakskode: Tiltakskode,
    }): CancelablePromise<Array<Tiltaksgjennomforing>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/tiltakstyper/{tiltakskode}/tiltaksgjennomforinger',
            path: {
                'tiltakskode': tiltakskode,
            },
            errors: {
                404: `the specified tiltakstype was not found.`,
            },
        });
    }

    /**
     * @returns Tiltaksgjennomforing Array of tiltaksgjennomføringer.
     * @throws ApiError
     */
    public static getTiltaksgjennomforinger({
        search,
        innsatsgrupper,
        tiltakstyper,
    }: {
        /** Search for tiltaksgjennomforinger **/
        search?: string,
        /** Innsatsgruppefilter **/
        innsatsgrupper?: Array<number>,
        /** Tiltakstypefilter **/
        tiltakstyper?: Array<number>,
    }): CancelablePromise<Array<Tiltaksgjennomforing>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/tiltaksgjennomforinger',
            query: {
                'search': search,
                'innsatsgrupper': innsatsgrupper,
                'tiltakstyper': tiltakstyper,
            },
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
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/tiltaksgjennomforinger/{id}',
            path: {
                'id': id,
            },
            errors: {
                404: `The specified tiltaksgjennomføring was not found.`,
            },
        });
    }

}