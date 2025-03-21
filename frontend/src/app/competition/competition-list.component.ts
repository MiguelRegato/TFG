import { Component, OnInit } from '@angular/core';
import { Competition } from '../models/competition';
import { CompetitionService } from '../service/competition.service';
import { NavigationStart, Router } from '@angular/router';
import { TokenService } from '../service/token.service';
import { UserService } from '../service/user.service';

@Component({
  selector: 'app-competition-list',
  templateUrl: './competition-list.component.html',
  styleUrls: ['./competition-list.component.css']
})
export class CompetitionListComponent implements OnInit {

  isProfessor = false;
  isStudent = false;
  isAdmin = false;
  roles: string[] = [];

  competitions: Competition[] = [];
  noCompetitions = true;

  professorName: string = '';

  constructor(private competitionService: CompetitionService, private router: Router, private tokenService: TokenService, private userService: UserService) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationStart) {
        if (event.navigationTrigger == 'popstate') {
          if (this.isAdmin)
            this.router.navigate(['/usuarios']);
          else
            this.router.navigate(['/']);
        }
      }
    });
  }

  ngOnInit(): void {
    this.roles = this.tokenService.getAuthorities();
    this.roles.forEach(rol => {
      if (rol === 'ROLE_PROFESSOR') {
        this.isProfessor = true;
        this.professorName = this.tokenService.getUserName()!;
        this.loadCompetitionsProfessor();
      } else if (rol === 'ROLE_STUDENT') {
        this.isStudent = true;
        this.loadCompetitionsStudent();
      } else {
        this.isAdmin = true;
        this.professorName = history.state.professorName;
        this.loadCompetitionsProfessor();
      }
    })
  }

  loadCompetitionsStudent() {
    this.userService.getCompetitionsByUserName(this.tokenService.getUserName()!).subscribe({
      next: (data) => {
        if (data.length != 0) {
          this.competitions = data;
          this.noCompetitions = false;
        } else {
          this.noCompetitions = true;
        }
      },
      error: (error) => {
        console.error('Error consiguiendo las competiciones', error);
      }
    });
  }

  loadCompetitionsProfessor(): void {
    this.competitionService.getCompetitionsByProfessor(this.professorName).subscribe({
      next: (data) => {
        this.competitions = data;
      },
      error: (error) => {
        console.error('Error consiguiendo las competiciones', error);
      }
    });
  }

  createCompetition() {
    this.router.navigate(['/nuevaCompeticion'], { state: { professorName: this.professorName } });
  }

  viewContests(competitionName: string, competitionId: number): void {
    this.router.navigate(['/' + competitionName + '/concursos'], { state: { competitionId, professorName: this.professorName } });
  }

  deleteCompetition(id: number): void {
    const confirmDelete = confirm('¿Está seguro de que desea eliminar esta competición?');
    if (confirmDelete) {
      this.competitionService.deleteCompetition(id).subscribe({
        next: () => {
          alert('Competición eliminada con éxito');
          this.loadCompetitionsProfessor();
        },
        error: (err) => console.error('Error al eliminar la competición:', err)
      });
    }
  }

  viewStudents(competitionName: string, competitionId: number): void {
    this.router.navigate(['/' + competitionName + '/alumnos'], { state: { competitionId: competitionId, professorName: this.professorName } });
  }
}
