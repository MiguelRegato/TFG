import { Competition } from './competition';
import { Wordle } from './wordle';

export class Contest {
    id!: number;
    contestName!: string;
    competition!: Competition;
    startDate!: Date;
    endDate!: Date;
    numTries!: number;
    useDictionary!: boolean;
    useExternalFile!: boolean;
    fileRoute!: string;
    wordles!: Wordle[];

    constructor(contestName: string,
        startDate: Date,
        endDate: Date,
        numTries: number,
        useDictionary: boolean,
        useExternalFile: boolean,
        fileRoute: string,
        competition: Competition,
        wordles: Wordle[] = []) {
        this.contestName = contestName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.numTries = numTries;
        this.useDictionary = useDictionary;
        this.useExternalFile = useExternalFile;
        this.fileRoute = fileRoute;
        this.competition = competition;
        this.wordles = wordles;
    }
}