{{/* Имя чарта */}}
{{- define "postgres-db.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/* Полное имя (в Umbrella это будет my-bank-account-db) */}}
{{- define "postgres-db.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}