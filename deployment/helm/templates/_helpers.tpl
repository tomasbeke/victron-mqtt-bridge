
{{- define "labels.standard" -}}
app.kubernetes.io/name: {{ .Chart.Name  | trunc 63 | trimSuffix "-" }}
app.kubernetes.io/component: auth
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/version: {{ .Values.image.version | quote }}
helm.sh/chart: {{ template "names.chart" . }}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "names.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Labels to use on deploy.spec.selector.matchLabels and svc.spec.selector
*/}}
{{- define "labels.matchLabels" -}}
app.kubernetes.io/name: {{ .Chart.Name  | trunc 63 | trimSuffix "-" }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: auth
{{- end -}}
